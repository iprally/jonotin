(ns jonotin.core
  (:import (org.threeten.bp Duration)
           (com.google.protobuf ByteString)
           (com.google.api.gax.batching BatchingSettings)
           (com.google.api.gax.core InstantiatingExecutorProvider)
           (com.google.api.core ApiFutureCallback
                                ApiFutures
                                ApiService$Listener)
           (com.google.common.util.concurrent MoreExecutors)
           (com.google.cloud.pubsub.v1 Publisher
                                       MessageReceiver
                                       Subscriber)
           (com.google.pubsub.v1 ProjectTopicName
                                 PubsubMessage
                                 ProjectSubscriptionName)))

(defn- get-executor-provider [{:keys [executor-thread-count]}]
  (let [executor-provider-builder (cond-> (InstantiatingExecutorProvider/newBuilder)
                                    executor-thread-count (.setExecutorThreadCount executor-thread-count))]
    (.build executor-provider-builder)))

(defn subscribe! [{:keys [project-name subscription-name handle-msg-fn handle-error-fn options]}]
  (let [subscription-name-obj (ProjectSubscriptionName/format project-name subscription-name)
        msg-receiver (reify MessageReceiver
                       (receiveMessage [_ message consumer]
                         (let [data (.toStringUtf8 (.getData message))]
                           (try
                            (handle-msg-fn data)
                            (.ack consumer)
                            (catch Exception e
                              (if (some? handle-error-fn)
                                (let [error-response (handle-error-fn e)]
                                  (if (or (nil? error-response)
                                          (:ack error-response))
                                    (.ack consumer)
                                    (.nack consumer)))
                                (do
                                  (.ack consumer)
                                  (throw e))))))))
        subscriber-builder (cond-> (Subscriber/newBuilder subscription-name-obj msg-receiver)
                             (:parallel-pull-count options) (.setParallelPullCount (:parallel-pull-count options))
                             (:executor-thread-count options) (.setExecutorProvider (get-executor-provider options)))
        subscriber (.build subscriber-builder)
        listener (proxy [ApiService$Listener] []
                   (failed [from failure]
                     (println "Jonotin failure with msg handling -" failure)))]
    (.addListener subscriber listener (MoreExecutors/directExecutor))
    (.awaitRunning (.startAsync subscriber))
    (.awaitTerminated subscriber)))

(defn publish! [{:keys [project-name topic-name messages]}]
  (when (> (count messages) 10000)
    (throw (ex-info "Message count over safety limit"
                    {:type :jonotin/batch-size-limit
                     :message-count (count messages)}))) 
  (let [topic (ProjectTopicName/of project-name topic-name)
        batching-settings (-> (BatchingSettings/newBuilder)
                              (.setRequestByteThreshold 1000)
                              (.setElementCountThreshold 10)
                              (.setDelayThreshold (Duration/ofMillis 1000))
                              .build)
        publisher (-> (Publisher/newBuilder topic)
                      (.setBatchingSettings batching-settings)
                      .build)
        callback-fn (reify ApiFutureCallback
                      (onFailure [_ t]
                                   (throw (ex-info "Failed to publish message"
                                                   {:type :jonotin/publish-failure
                                                    :message t})))
                      (onSuccess [_ _result]
                                   ()))
        publish-msg-fn (fn [msg-str]
                         (let [msg-builder (PubsubMessage/newBuilder)
                               data (ByteString/copyFromUtf8 msg-str)
                               msg (-> msg-builder
                                       (.setData data)
                                       .build)
                               msg-future (.publish publisher msg)]
                           (ApiFutures/addCallback msg-future callback-fn (MoreExecutors/directExecutor))
                           msg-future))
        futures (map publish-msg-fn messages)
        message-ids (.get (ApiFutures/allAsList futures))]
    (.shutdown publisher)
    (.awaitTermination publisher 5 java.util.concurrent.TimeUnit/MINUTES)
    {:delivered-messages (count message-ids)}))
