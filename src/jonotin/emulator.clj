(ns jonotin.emulator
  (:import (com.google.api.gax.core NoCredentialsProvider)
           (com.google.api.gax.grpc GrpcTransportChannel)
           (com.google.api.gax.rpc FixedTransportChannelProvider)
           (io.grpc ManagedChannelBuilder)))

(def pubsub-emulator-host (System/getenv "PUBSUB_EMULATOR_HOST"))

(def host-configured? (boolean pubsub-emulator-host))

(defn- ensure-host-configured []
  (when-not host-configured?
    (throw (ex-info "Unable to use emulator without PUBSUB_EMULATOR_HOST"
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
