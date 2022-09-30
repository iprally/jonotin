# jonotin

Dead-simple Google Cloud Pub/Sub from Clojure. jonotin is a never used Finnish word for a thing that queues. Read more about jonotin from [IPRally blog](https://www.iprally.com/news/google-cloud-pubsub-with-clojure).

## Latest version

Leiningen/Boot
```clj
[jonotin "0.3.2"]
```

Clojure CLI/deps.edn
```clj
jonotin {:mvn/version "0.3.2"}
```

Gradle
```clj
compile 'jonotin:jonotin:0.3.2'
```

Maven
```clj
<dependency>
  <groupId>jonotin</groupId>
  <artifactId>jonotin</artifactId>
  <version>0.3.2</version>
</dependency>
```

### Publish!

```clj
(require '[jonotin.core :as jonotin])

(jonotin/publish! {:project-name "my-gcloud-project-id"
                   :topic-name "my-topic"
                   :messages ["msg1" "msg2"]})
```

### Subscribe!

Subscribe processes messages from the queue concurrently.
```clj
(require '[jonotin.core :as jonotin])

(jonotin/subscribe! {:project-name "my-gcloud-project-id"
                     :subscription-name "my-subscription-name"
                     :handle-msg-fn (fn [msg]
                                      (println "Handling" msg)
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
                                      (println "Handling" msg)
                     :handle-error-fn (fn [e]
                                        (println "Oops!" e))})
  ```
