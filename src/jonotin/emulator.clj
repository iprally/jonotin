(ns jonotin.emulator
  (:import (com.google.api.gax.core NoCredentialsProvider)
           (com.google.api.gax.grpc GrpcTransportChannel)
           (com.google.api.gax.rpc FixedTransportChannelProvider)
           (com.google.cloud.pubsub.v1 SubscriptionAdminClient SubscriptionAdminSettings TopicAdminClient TopicAdminSettings)
           (com.google.pubsub.v1 PushConfig SubscriptionName TopicName)
           (io.grpc ManagedChannelBuilder)))

(def pubsub-emulator-host (System/getenv "PUBSUB_EMULATOR_HOST"))

(def host-configured? (boolean pubsub-emulator-host))

(defn ensure-host-configured []
  (when-not host-configured?
    (throw (ex-info "PUBSUB_EMULATOR_HOST is required for using Google Cloud Pub/Sub emulator"
                    {:type :jonotin/emulator-host-not-configured}))))

(defn build-channel []
  (ensure-host-configured)
  (-> pubsub-emulator-host
      (ManagedChannelBuilder/forTarget)
      (.usePlaintext)
      (.build)))

(defn set-builder-options [builder emulator-channel]
  (ensure-host-configured)
  (-> builder
      (.setCredentialsProvider (NoCredentialsProvider/create))
      (.setChannelProvider (-> emulator-channel
                               (GrpcTransportChannel/create)
                               (FixedTransportChannelProvider/create)))))

(defn- set-admin-client-builder-options [builder emulator-channel]
  (ensure-host-configured)
  (-> builder
      (.setCredentialsProvider (NoCredentialsProvider/create))
      (.setTransportChannelProvider (-> emulator-channel
                                        (GrpcTransportChannel/create)
                                        (FixedTransportChannelProvider/create)))))

(defn with-topic-admin-client [f]
  (let [channel (build-channel)
        topic-admin-settings (-> (TopicAdminSettings/newBuilder)
                                 (set-admin-client-builder-options channel)
                                 (.build))
        topic-client (TopicAdminClient/create ^TopicAdminSettings topic-admin-settings)]
    (try
      (f topic-client)
      (finally
        (.shutdown topic-client)
        (.shutdown channel)))))

(defn with-subscription-admin-client [f]
  (let [channel (build-channel)
        subscription-admin-settings (-> (SubscriptionAdminSettings/newBuilder)
                                        (set-admin-client-builder-options channel)
                                        (.build))
        subscription-client (SubscriptionAdminClient/create ^SubscriptionAdminSettings subscription-admin-settings)]
    (try
      (f subscription-client)
      (finally
        (.shutdown subscription-client)
        (.shutdown channel)))))

(defn create-topic [project-name topic-name]
  (with-topic-admin-client
    #(.createTopic % (TopicName/of project-name topic-name))))

(defn get-topic [project-name topic-name]
  (with-topic-admin-client
    #(.getTopic % (TopicName/of project-name topic-name))))

(defn delete-topic [project-name topic-name]
  (with-topic-admin-client
    (fn [client]
      (.deleteTopic client (TopicName/of project-name topic-name))
      true)))

(defn create-subscription [project-name topic-name subscription-name & {:keys [ack-deadline-seconds]
                                                                        :or {ack-deadline-seconds 10}}]
  (with-subscription-admin-client
    (fn [client]
      (let [project-subscription (SubscriptionName/of project-name subscription-name)
            project-topic (TopicName/of project-name topic-name)
            push-config (PushConfig/getDefaultInstance)]
        (.createSubscription client project-subscription project-topic push-config ack-deadline-seconds)))))

(defn get-subscription [project-name subscription-name]
  (with-subscription-admin-client
    #(.getSubscription % (SubscriptionName/of project-name subscription-name))))

(defn delete-subscription [project-name subscription-name]
  (with-subscription-admin-client
    (fn [client]
      (.deleteSubscription client (SubscriptionName/of project-name subscription-name))
      true)))
