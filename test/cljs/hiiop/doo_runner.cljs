(ns hiiop.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [hiiop.core-test]))

(doo-tests 'hiiop.core-test)

