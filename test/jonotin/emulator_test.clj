(ns jonotin.emulator-test
  (:require [clojure.test :refer :all]
            [jonotin.emulator :as emulator]
            [jonotin.test-helpers :refer :all]))

(deftest topic-test
  (let [topic-name (unique-topic-name)]
    (is (= (emulator/create-topic project-name topic-name)
           (emulator/get-topic project-name topic-name)))
    (is (emulator/delete-topic project-name topic-name))))

(deftest subscription-test
  (let [topic-name (unique-topic-name)
        subscription-name (unique-subscription-name)]
    (is (emulator/create-topic project-name topic-name))
    (is (= (emulator/create-subscription project-name topic-name subscription-name)
           (emulator/get-subscription project-name subscription-name)))
    (is (emulator/delete-subscription project-name subscription-name))))
