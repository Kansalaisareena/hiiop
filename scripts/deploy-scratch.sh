lein uberjar
aws s3 sync resources/public/ s3://hiiop-scratch --delete
heroku git:remote --app hiiop-scratch
heroku maintenance:on --app hiiop-scratch
DATABASE_URL="$(heroku config:get "DATABASE_URL" --app hiiop-scratch)?sslmode=require" lein migratus migrate
HEROKU_APP="hiiop-scratch" lein heroku deploy-uberjar
heroku maintenance:off --app hiiop-scratch