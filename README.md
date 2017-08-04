Simple Http Server that renders HTML pages using Selenium.

Used for SEO optimisation (solving problems related to technology such as: AngularJS, webcomponents, polymer)


Build
```shell
mvn package
```


Access the machine  (nextp-vm2b)

Stop current container and restart selenium server (nextp-vm2b)

```shell
docker ps #to check CONTAINER_ID of the running instance
docker stop $CONTAINER_ID
docker run --name selenium3 -d -p 4444:4444 -p 5900:5900 -e JAVA_OPTS=-Xmx2048m --add-host=old.nextprot.org:127.0.0.1 --add-host=www.google-analytics.com:127.0.0.1 -v /dev/shm:/dev/shm selenium/standalone-chrome:3.4.0-dysprosium

#Other selenium configuration
#docker run -d --rm --name selenium1 --shm-size=2048m --cap-add=SYS_ADMIN --add-host=old.nextprot.org:127.0.0.1 --add-host=www.google-analytics.com:127.0.0.1 -p=127.0.0.1:4444:4444   yukinying/chrome-headless-browser-selenium
```

Run sparender (nextp-vm2b)

```shell
nohup java -Xmx2g -Dcom.sun.management.jmxremote.port=5000 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dehcache.path=/work/sparender/cache -jar /work/sparender/dist/app.jar &> /work/sparender/logs.txt &
```

Access your browser at: http://localhost:8082/http://alpha-search.nextprot.org/about/human-proteome for an example of plain html file


Configure apache:
```shell
            #Rewrite rule added for selenium dynamic
            RewriteCond %{HTTP_USER_AGENT} googlebot|google|baiduspider|facebookexternalhit|twitterbot|rogerbot|linkedinbot|embedly|quora\ link\ preview|showyoubot|outbrain|pinterest|slackbot|vkShare|W3C_Validator [NC,OR]
            RewriteCond %{QUERY_STRING} _escaped_fragment_
            RewriteRule ^(?!.*?(webcomponents.*|static.*|\.js|\.htm|\.css|\.xml|\.less|\.png|\.jpg|\.jpeg|\.gif|\.pdf|\.doc|\.txt|\.ico|\.rss|\.zip|\.mp3|\.rar|\.exe|\.wmv|\.doc|\.avi|\.ppt|\.mpg|\.mpeg|\.tif|\.wav|\.mov|\.psd|\.ai|\.xls|\.mp4|\.m4a|\.swf|\.dat|\.dmg|\.iso|\.flv|\.m4v|\.torrent|\.ttf|\.woff))(.*) http://nextp-vm2b.vital-it.ch:8082/https://www.nextprot.org%{REQUEST_URI} [P]

```
