nohup java -jar /home/www/yingwans/ruoyi-admin.jar \
    --spring.profiles.active=prod \
    --spring.data.redis.host='127.0.0.1' \
    --spring.data.redis.port=6379 \
    --spring.data.redis.password='admin@8899' \
    --spring.data.redis.database=8 > /home/www/yingwans/ruoyi-admin.log 2>&1 &