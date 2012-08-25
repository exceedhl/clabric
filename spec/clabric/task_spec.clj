(ns clabric.task-spec
  (:use 
   [speclj.core]
   [clabric.task]))

(defn distributed-action []
  (distribute (fn [option]
                option)))

(defn local-action []
  (local (fn [option]
           option)))

(describe "Task"
  
  (context "with single host"
    (it "should distribute to the host when execute with options"
      (deftask t1 ["host1"] "task t1"
        (distributed-action))
      (let [result (execute t1 :opt1 "opt1")]
        (should= 1 (count result))
        (should= {:host "host1" :opt1 "opt1"} (first result))))

    (it "should distribute to the host when execute without options"
      (deftask t1 ["host1"] "task t1"
        (distributed-action))
      (let [result (execute t1)]
        (should= 1 (count result))
        (should= {:host "host1"} (first result))))

    (it "should pass options to local commands"
      (deftask t1 ["host1"] "task t1"
        (local-action))
      (let [result (execute t1 :opt1 "opt1")]
        (should= {:opt1 "opt1"} result))))

  (context "with multiple hosts"

    (it "should distribute to those hosts when execute with options"
      (deftask t1 ["host1" "host2"] "task t1"
        (distributed-action))
      (let [result (execute t1 :opt1 "opt1")]
        (should= 2 (count result))
        (should= {:host "host1" :opt1 "opt1"} (first result))
        (should= {:host "host2" :opt1 "opt1"} (second result))))

    (it "should distribute to those hosts when execute without options"
      (deftask t1 ["host1" "host2"] "task t1"
        (distributed-action))
      (let [result (execute t1)]
        (should= 2 (count result))
        (should= {:host "host1"} (first result))
        (should= {:host "host2"} (second result))))

    (it "should not distribute to hosts for local commands"
      (deftask t1 ["host1" "host2"] "task t1"
        (local-action))
      (let [result (execute t1 :opt1 "opt1")]
        (should= {:opt1 "opt1"} result))))

  (context "with no hosts"

    (it "should not distribute any actions during execution"
      (deftask t1 [] "task t1"
        (distributed-action))
      (let [result (execute t1)]
        (should= 0 (count result))))

    (it "should still run local command"
      (deftask t1 [] "task t1"
        (local-action))
      (let [result (execute t1 :opt1 "opt1")]
        (should= {:opt1 "opt1"} result))))

  (context "definition"

    (it "define with name and description"
      (deftask t1 [] "task t1")
      (should= "t1" (:name t1))
      (should= "task t1" (:description t1)))

    (it "define with hosts"
      (deftask t1 ["host1" "host2"] "task t1")
      (should= ["host1" "host2"] (:hosts t1)))

    (it "should merge duplicate hosts while define with duplicate host entries"
      (deftask t1 ["host1" "host1" "host2"] "task t1")
      (should= ["host1" "host2"] (:hosts t1)))))

(run-specs)