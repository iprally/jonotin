(ns jonotin.core-test
  (:require [clojure.test :refer :all]
            [jonotin.core :as core]
            [jonotin.emulator :as emulator]
            [jonotin.test-helpers :refer :all]))

(use-fixtures :once ensure-emulator-host-configured)

(deftest subscribe!-test
  (let [successful-messages (atom 0)
        errored-messages (atom 0)
        topic-name (unique-topic-name)
        subscription-name (unique-subscription-name)]
    (emulator/create-topic project-name topic-name)
    (emulator/create-subscription project-name topic-name subscription-name)
    (in-parallel
      (core/subscribe! {:project-name project-name
                        :subscription-name subscription-name
                        :handle-msg-fn #(case %
                                          "normal" (swap! successful-messages inc)
                                          "exceptional-retry" (throw (ex-info "" {::retry? true}))
                                          "exceptional-no-retry" (throw (ex-info "" {::retry? false})))
                        :handle-error-fn (fn [e]
                                           (swap! errored-messages inc)
                                           (if (::retry? (ex-data e))
                                             {:ack false}
                                             {:ack true}))}))
    (core/publish! {:project-name project-name
                    :topic-name topic-name
                    :messages ["normal"
                               "exceptional-retry"
                               "normal"
                               "exceptional-no-retry"
                               "normal"]})
    (Thread/sleep 500)
    (is (= 3 @successful-messages))
    (is (= 6 @errored-messages))))

(deftest publish!-test
  (let [topic-name (unique-topic-name)]
    (emulator/create-topic project-name topic-name)
    (is (= {:delivered-messages 2}
           (core/publish! {:project-name project-name
                           :topic-name topic-name
                           :messages ["hello" "goodbye"]})))))
