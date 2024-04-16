# jonotin

Dead-simple Google Cloud Pub/Sub from Clojure. jonotin is a never used Finnish word for a thing that queues. Read more about jonotin from [IPRally blog](https://www.iprally.com/news/google-cloud-pubsub-with-clojure).

## Latest version

Leiningen/Boot
```clj
[jonotin "0.4.0"]
```

Clojure CLI/deps.edn
```clj
jonotin {:mvn/version "0.4.0"}
```

Gradle
```clj
compile 'jonotin:jonotin:0.4.0'
```

Maven
```clj
<dependency>
  <groupId>jonotin</groupId>
  <artifactId>jonotin</artifactId>
  <version>0.4.0</version>
</dependency>
```

### Publish!

Publish messages to topic. Thresholds can be configured through options:
- Delay Threshold: Counting from the time that the first message is queued, once this delay has passed, then send the batch. The default value is 100 millisecond, which is good for large amount of messages.
- Message Count Threshold: Once this many messages are queued, send all of the messages in a single call, even if the delay threshold hasn't elapsed yet. The default value is 10 messages.
- Request Byte Threshold: Once the number of bytes in the batched request reaches this threshold, send all of the messages in a single call, even if neither the delay or message count thresholds have been exceeded yet. The default value is 1000 bytes.

```clj
(require '[jonotin.core :as jonotin])

(jonotin/publish! {:project-name "my-gcloud-project-id"
                   :topic-name "my-topic"
                   :messages ["msg1" "msg2"]
		   :options {:request-byte-threshold 100
                             :element-count-threshold 10
                             :delay-threshold 1000}})
```

### Subscribe!

Subscribe processes messages from the queue concurrently.
```clj
(require '[jonotin.core :as jonotin])

(jonotin/subscribe! {:project-name "my-gcloud-project-id"
                     :subscription-name "my-subscription-name"
                     :handle-msg-fn (fn [msg]
                                      (println "Handling" msg))
                     :handle-error-fn (fn [e]
                                        (println "Oops!" e))})
  ```

Error handler function supports return value to determine if the message should be acknowledged or not.
```clj
{:ack boolean}
```

Subscribe with concurrency control.
```clj
(require '[jonotin.core :as jonotin])

(jonotin/subscribe! {:project-name "my-gcloud-project-id"
                     :subscription-name "my-subscription-name"
                     :options {:parallel-pull-count 2
                               :executor-thread-count 4}
                     :handle-msg-fn (fn [msg]
                                      (println "Handling" msg))
                     :handle-error-fn (fn [e]
                                        (println "Oops!" e))})
  ```

## Testing

jonotin supports Google Cloud Pub/Sub emulator. When environment variable `PUBSUB_EMULATOR_HOST` is set, then jonotin will use emulator instead of the real GCP Pub/Sub API.

To set up the emulator, follow [Testing apps locally with the emulator](https://cloud.google.com/pubsub/docs/emulator) guide for setting up the emulator.

The guide features a command for setting `PUBSUB_EMULATOR_HOST`:

```bash
$(gcloud beta emulators pubsub env-init)
```

Run your application and witness jonotin diligently using the emulator.

Note that on the first run, no topics or subscriptions exist in the emulator. jonotin includes helpers for creating/removing those:

```clojure
(require '[jonotin.emulator :as emulator])

;; Create a topic
(emulator/create-topic "my-project" "my-topic")

;; Delete a topic
(emulator/delete-topic "my-project" "my-topic")

;; Create a pull subscription
(emulator/create-pull-subscription "my-project" "my-topic" "my-pull-subscription")

;; Delete a pull subscription
(emulator/delete-pull-subscription "my-project" "my-topic" "my-pull-subscription")

;; Create a push subscription
(emulator/create-push-subscription "my-project" "my-topic" "my-push-subscription")

;; Delete a push subscription
(emulator/delete-push-subscription "my-project" "my-topic" "my-push-subscription")
```
