# jonotin

Dead-simple Google Cloud Pub/Sub from Clojure. jonotin is a never used Finnish word for a thing that queues. Read more about jonotin from [IPRally blog](https://www.iprally.com/news/google-cloud-pubsub-with-clojure).

## Latest version

Leiningen/Boot
```clj
[jonotin "0.4.2"]
```

Clojure CLI/deps.edn
```clj
jonotin {:mvn/version "0.4.2"}
```

Gradle
```clj
compile 'jonotin:jonotin:0.4.2'
```

Maven
```clj
<dependency>
  <groupId>jonotin</groupId>
  <artifactId>jonotin</artifactId>
  <version>0.4.2</version>
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

## Testing your application

jonotin supports Google Cloud Pub/Sub emulator. When environment variable `PUBSUB_EMULATOR_HOST` is set, then jonotin will use emulator instead of the real GCP Pub/Sub API.

To set up the emulator, follow [Testing apps locally with the emulator](https://cloud.google.com/pubsub/docs/emulator) guide for setting up the emulator.

Once your emulator is up-and-running, configure `PUBSUB_EMULATOR_HOST`:

```bash
$(gcloud beta emulators pubsub env-init) && echo $PUBSUB_EMULATOR_HOST
# => localhost:8085
```

Now run your application and witness jonotin diligently using the emulator.

Note that emulator is ephemeral and no topics or subscriptions exist when it starts. jonotin includes helpers for creating/removing those:

```clojure
(require '[jonotin.emulator :as emulator])

;; Create a topic
(emulator/create-topic "project-name" "topic-name")

;; Get the topic
(emulator/get-topic "project-name" "topic-name")

;; Delete the topic
(emulator/delete-topic "project-name" "topic-name")

;; Create a subscription
(emulator/create-subscription "project-name" "topic-name" "subscription-name")

;; Create a subscription with custom ack-deadline-seconds
(emulator/create-subscription "project-name" "topic-name" "subscription-name" {:ack-deadline-seconds 600})

;; Get the subscription
(emulator/get-subscription "project-name" "subscription-name")

;; Delete the subscription
(emulator/delete-subscription "project-name" "subscription-name")
```

To be sure that jonotin in your test suite targets Pub/Sub emulator, use

```clojure
(emulator/ensure-host-configured)
```

where appropriate. This function will throw if `PUBSUB_EMULATOR_HOST` is not configured.

# Development

## Testing jonotin

Once you've set up the Google Cloud Pub/Sub emulator, start the emulator for jonotin test project:

```bash
gcloud beta emulators pubsub start --project=jonotin-test-emulator
```

In another shell session, configure `PUBSUB_EMULATOR_HOST` and run the tests:

```bash
$(gcloud beta emulators pubsub env-init) && echo $PUBSUB_EMULATOR_HOST
# => localhost:8085

lein test
```
