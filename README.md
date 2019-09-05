# jonotin

Dead-simple Google cloud Pub/Sub from Clojure. jonotin is a never used Finnish word for a thing that queues.

## Usage

Copy the code. It's too simple for library and we don't have enough time for updating the binaries.

### Publish!

```clj
(jonotin/publish! {:project-name "my-gcloud-project"
                   :topic-name "my-topic"
                   :messages ["msg1" "msg2"]})
```

### Subscribe!

Subscribe processed messages from the queue until the queue is empty. Batch size is the number of messages fetched from the queue at once.
```clj
(jonotin/subscribe! {:project-name "my-gcloud-project"
                     :subscription-name "my-subscription-name"
                     :batch-size 10
                     :handle-msg-fn (fn [msg]
                                      (println "Handling" msg)
                     :handle-error-fn (fn [e]
                                        (println "Oops!" e))})
  ```
We use it with at-at, so that subscribe is tried every 30s when queue is empty.
```clj
(let [pool (at-at/mk-pool)]
    (at-at/interspaced 30000
                       (fn []
                         (jonotin/subscribe! {:project-name (config/get-property [:pubsub :project-name])
                                              :subscription (config/get-property [:pubsub :subscription])
                                              :batch-size 10
                                              :handle-msg-fn handle-msg!
                                              :handle-error-fn handle-error!})
                         (log/info "All EP patents imported from pub/sub queue"))
                       pool))
```
