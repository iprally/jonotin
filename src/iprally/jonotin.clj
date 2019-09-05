(ns iprally.jonotin
  (:import (com.google.cloud.pubsub.v1 Publisher
                                       Subscriber)
           (com.google.pubsub.v1 PubsubMessage
                                 ProjectTopicName
                                 ProjectSubscriptionName
                                 AcknowledgeRequest
                                 PullRequest)
           (com.google.cloud.pubsub.v1.stub GrpcSubscriberStub
                                            SubscriberStubSettings)
           (com.google.protobuf ByteString)))

(defn subscribe! [{:keys [project-name subscription-name batch-size handle-msg-fn handle-error-fn]}]
  (let [transport-provider (.build (SubscriberStubSettings/defaultGrpcTransportProviderBuilder))
        subscription-name-obj (ProjectSubscriptionName/format project-name subscription-name)
        subscriber (GrpcSubscriberStub/create (-> (SubscriberStubSettings/newBuilder)
                                                  (.setTransportChannelProvider transport-provider)
                                                  .build))
        acknowledge-msg! (fn [ack-id]
                           (let [request (-> (AcknowledgeRequest/newBuilder)
                                             (.setSubscription subscription-name-obj)
                                             (.addAckIds ack-id)
                                             .build)]
                             (.call (.acknowledgeCallable subscriber) request)))
        pull-request (-> (PullRequest/newBuilder)
                         (.setReturnImmediately true)
                         (.setSubscription subscription-name-obj)
                         (.setMaxMessages batch-size)
                         .build)
        process-msg-fn (fn [m]
                         (let [msg (-> m
                                       .getMessage
                                       .getData
                                       .toStringUtf8)
                               ack-id (.getAckId m)]
                           (try
                             (handle-msg-fn msg)
                             (catch Exception e
                               (if (some? handle-error-fn)
                                 (handle-error-fn e)
                                 (throw e)))
                             (finally
                               (acknowledge-msg! ack-id)))))]
    (loop []
      (let [messages (.getReceivedMessagesList (.call (.pullCallable subscriber) pull-request))]
        (if (empty? messages)
          (do
            (.shutdown subscriber)
            (.awaitTermination subscriber 1 java.util.concurrent.TimeUnit/MINUTES))
          (do
            (doall (map process-msg-fn messages))
            (recur)))))))

(defn publish! [{:keys [project-name topic-name messages]}]
  (let [topic (ProjectTopicName/of project-name topic-name)
        publisher (.build (Publisher/newBuilder topic))
        msg-builder (PubsubMessage/newBuilder)]
    (doall
      (map (fn [msg-str]
             (let [data (ByteString/copyFromUtf8 msg-str)
                   msg (-> msg-builder
                           (.setData data)
                           .build)]
               (.publish publisher msg)))
           messages))
    (.shutdown publisher)
    (.awaitTermination publisher 1 java.util.concurrent.TimeUnit/MINUTES)))
