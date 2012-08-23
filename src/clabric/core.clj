(ns clabric.core)

(def ^:dynamic *hosts* [])
(def ^:dynamic *options* {})

(defrecord Task [^String name hosts ^String description body])

(defmacro deftask
  [name hosts description & body]
  `(def ~name (Task. (name '~name) ~hosts ~description (fn [] ~@body))))

(defn execute
  [task & options]
  (binding [*hosts* (:hosts task)
            *options* (merge *options* (apply array-map options))]
   ((:body task))))

;; (def ^:dynamic *options* {})

;; (deftype Task
;;     ...)

;; ;;; register task
;; (deftask deploy-app app-hosts           ; [app-hosts db-hosts]
;;   (run "yum makecache" :user "root")
;;   (run "yum install -y httpd xxx" :private_key_path "key_file")
;;   (cmd "turn off monitoring" :timeout 60)
;;   (put "some-file" "/tmp/chef.json")
;;   (println "hello, world")
;;   (run "chef-solo -r solo.rb -j chef.json")
;;   (cmd "turn on monitoring")
;;   (cmd "switch LB"))

;; (deftask provision-db []
;;   (execute create-db)
;;   (execute deploy-db))

;; (execute create-rcrm)
;; (execute deploy-app)
;; (execute deploy-rcrm)

;; (execute provision-db :user "root")

;; (defn cmd [command & options]
;;   (...))

;; (defn run [command & options]
;;   (let [p (merge *execution-params* (apply array-map options))]
;;    (parallel (map (fn [host]
;;                     (fn []
;;                       (let [params (merge p {:host host})]
;;                         (ssh-exec (ssh-session params) command))))
;;                   *hosts*))))

;;; deploy create-app -Duser=vagrant -Dport=422 -Dprivate_key_path="...."

