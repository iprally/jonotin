(ns jonotin.core
  (:require [jonotin.emulator :as emulator])
  (:import (com.google.api.core ApiFutureCallback
                                ApiFutures
                                ApiService$Listener)
           (com.google.api.gax.batching BatchingSettings)
           (com.google.api.gax.core InstantiatingExecutorProvider)
           (com.google.cloud.pubsub.v1 MessageReceiver
                                       Publisher
                                       Subscriber)
           (com.google.common.util.concurrent MoreExecutors)
           (com.google.protobuf ByteString)
           (com.google.pubsub.v1 ProjectSubscriptionName
                                 ProjectTopicName
                                 PubsubMessage)
           (java.util.concurrent TimeUnit)
           (org.threeten.bp Duration)))

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
                             (catch Throwable e
                               (if (some? handle-error-fn)
                                 (let [error-response (handle-error-fn e)]
                                   (if (or (nil? error-response)
                                           (:ack error-response))
                                     (.ack consumer)
                                     (.nack consumer)))
                                 (do
                                   (.ack consumer)
                                   (throw e))))))))
        emulator-channel (when emulator/host-configured?
                           (emulator/build-channel))
        subscriber-builder (cond-> (Subscriber/newBuilder subscription-name-obj msg-receiver)
                             (:parallel-pull-count options) (.setParallelPullCount (:parallel-pull-count options))
                             (:executor-thread-count options) (.setExecutorProvider (get-executor-provider options))
                             emulator-channel (emulator/set-builder-options emulator-channel))
        subscriber (.build subscriber-builder)
        listener (proxy [ApiService$Listener] []
                   (failed [from failure]
                     (println "Jonotin failure with msg handling -" failure)))]
    (.addListener subscriber listener (MoreExecutors/directExecutor))
    (.awaitRunning (.startAsync subscriber))
    (.awaitTerminated subscriber)
    (some-> emulator-channel .shutdown)))

(defn publish! [{:keys [project-name topic-name messages options]}]
  (when (> (count messages) 10000)
    (throw (ex-info "Message count over safety limit"
                    {:type :jonotin/batch-size-limit
                     :message-count (count messages)})))
  (let [topic (ProjectTopicName/of project-name topic-name)
        batching-settings (-> (BatchingSettings/newBuilder)
                              (.setRequestByteThreshold (or (:request-byte-threshold options) 1000))
                              (.setElementCountThreshold (or (:element-count-threshold options) 10))
                              (.setDelayThreshold (Duration/ofMillis (or (:delay-threshold options) 100)))
                              .build)
        emulator-channel (when emulator/host-configured?
                           (emulator/build-channel))
        publisher-builder (cond-> (Publisher/newBuilder topic)
                            batching-settings (.setBatchingSettings batching-settings)
                            emulator-channel (emulator/set-builder-options emulator-channel))
        publisher (.build publisher-builder)]
    (try
      (let [callback-fn (reify ApiFutureCallback
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
        {:delivered-messages (count message-ids)})
      (finally
        (.shutdown publisher)
        (.awaitTermination publisher 5 TimeUnit/MINUTES)
        (some-> emulator-channel .shutdown)))))
