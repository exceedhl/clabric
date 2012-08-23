(ns clabric.core-spec
  (:use 
    [speclj.core]
    [clabric.core]))

(describe "Clabric"

  (it "should not set options if execute tasks without options"
     (deftask t1 ["host1"] "task t1"
       (should= ["host1"] *hosts*)
       (should= {} *options*))
     (execute t1))
  
   (it "should set options if execute tasks with options"
     (deftask t1 ["host1"] "task t1"
       (should= ["host1"] *hosts*)
       (should= {:opt1 "opt1" :opt2 "opt2"} *options*))
     (execute t1 :opt1 "opt1" :opt2 "opt2"))

   (it "should set options cascadingly tasks with nesting execution in task definition"
     (deftask t1 ["host1" "host2"] "task t1"
       (should= ["host1" "host2"] *hosts*)
       (should= {:opt1 "opt1" :opt2 "opt2" :opt3 "opt3"} *options*))
     (deftask t2 ["host3"] "task t2"
       (should= ["host3"] *hosts*)
       (should= {:opt1 "opt1" :opt2 "opt2"} *options*)
       (execute t1 :opt3 "opt3"))
     (execute t2 :opt1 "opt1" :opt2 "opt2"))
   
  (context "tasks"

    (it "define with name and description"
     (deftask t1 [] "task t1")
     (should= "t1" (:name t1))
     (should= "task t1" (:description t1)))

   (it "define with hosts"
     (deftask t1 ["host1" "host2"] "task t1")
     (should= ["host1" "host2"] (:hosts t1))))

  (context "actions"
    (it "should run commands on multiple hosts parallel")

    (it "should run commands with overriding options")))

(run-specs) 