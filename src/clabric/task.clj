(ns clabric.task)

(def ^{:dynamic true :private true} *hosts* [])
(def ^{:dynamic true :private true} *options* {})

(defrecord Task [^String name hosts ^String description body])

(defmacro deftask
  [name hosts description & body]
  `(def ~name (Task. (name '~name) ~hosts ~description (fn [] ~@body))))

(defn execute [task & options]
  (binding [*hosts* (:hosts task)
            *options* (merge *options* (apply array-map options))]
    ((:body task))))

(defn distribute [f]
  (let [options (map (fn [host]
                       (merge *options* {:host host})) *hosts*)]
    (map f (vec options))))

(defn local [f]
  (f *options*))
