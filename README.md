# clabric

A fabric like library/tool for streamlining the use of SSH for
application deployment or systems administration tasks.

## Usage

    (deftask t2 ["10.0.2.15"] "task t2"
      (run "ps aux")
      (upload "chef.rb" "/tmp/chef.rb")
      (cmd "ls -la")
      (put "some content\n" "/tmp/c.txt")
      (run "sudo cat /etc/passwd"))

    (execute t2 :user "vagrant" :private_key_path "~/.ssh/vagrant_private_key")

    (deftask t3 ["localhost"] "task t3"
      (cmd "uname -a")
      (cmd "ls -la")
      (execute t2))

    (execute t3)


## License

Copyright © 2012 exceedhl

Distributed under the Eclipse Public License, the same as Clojure.
