(ns jonotin.test-helpers
  (:require [jonotin.emulator :as emulator]))

(def project-name "jonotin-test-emulator")

(defn unique-topic-name []
  (str "test-topic-" (random-uuid)))

(defn unique-subscription-name []
  (str "test-subscription-" (random-uuid)))

(defn ensure-emulator-host-configured [f]
  (emulator/ensure-host-configured)
  (f))

(defmacro in-parallel [& body]
  `(.start (Thread. (fn [] ~@body))))
