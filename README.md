# hn-stats
Scala client for logging Hacker News karma (for a user) and score (for his submissions)

###Why
I wanted to track changes in my Hacker News karma over time. 

###Why Scala
I learnt Scala some time back but never got to develop anything using it. This would be my first step in that direction.

###How to use it
Pretty straight-forward. Import the project into any IDE and run Main.scala. Ohh and do not forget to copy config.sample.conf to config.conf and populate the fields.

###What excatly is it doing
It picks a username from config.conf, then its fetchs the submissions for that user from the [Hacker News API](https://github.com/HackerNews/API).

For a new user
* Its `userid`, `username` and `karma` are inserted into a new row into `user` table with `inserted_at` and `updated_at` set to current timestamp.
* `uid`, `updated_at` (=current timestamp) and `karma` is inserted in a new row in `user_score` table.
* All his submissions are downloaded and logged in `submission` table. Corresponding scores are logged in `submission_score` table.

For a repeated user
* The row corresponding to this user in `user` table is updated to reflect the new `karma` and `updated_at` ( = current timestamp).
* If there is any change in `karma`,a new row is inserted in `user_score` as before.
* All his new submissions are logged in `submission` table. Corresponding scores are logged in `submission_score` table.
* Scores corresponding to last few (set as `app.numberOfOldSubmissionsToCheck` ) submissions are also updated.
