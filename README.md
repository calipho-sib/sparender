Simple Http Server that renders HTML pages using Selenium.

Used for SEO optimisation (solving problems related to technology such as: AngularJS, webcomponents, polymer)


Build
```
mvn package
```
```

Access the machine  (nextp-vm2b)

Run selenium server (nextp-vm2b)
```shell 
docker ps (to kill any previous instance)
docker run -d --rm --name chrome --shm-size=1024m --cap-add=SYS_ADMIN --add-host=old.nextprot.org:127.0.0.1 -p=127.0.0.1:4444:4444   yukinying/chrome-headless-browser-selenium
```
Run sparender (nextp-vm2b)
```
nohup java -Dehcache.path=/work/sparender/cache -jar /work/sparender/dist/app.jar &> /work/sparender/logs.txt &
```

Access your browser at: http://localhost:8082/http://alpha-search.nextprot.org/about/human-proteome for an example of plain html file


Configure apache:
```shell
            #Rewrite rule added for selenium dynamic
            RewriteCond %{HTTP_USER_AGENT} googlebot|google|baiduspider|facebookexternalhit|twitterbot|rogerbot|linkedinbot|embedly|quora\ link\ preview|showyoubot|outbrain|pinterest|slackbot|vkShare|W3C_Validator [NC,OR]
            RewriteCond %{QUERY_STRING} _escaped_fragment_
            RewriteRule ^(?!.*?(webcomponents.*|static.*|\.js|\.htm|\.css|\.xml|\.less|\.png|\.jpg|\.jpeg|\.gif|\.pdf|\.doc|\.txt|\.ico|\.rss|\.zip|\.mp3|\.rar|\.exe|\.wmv|\.doc|\.avi|\.ppt|\.mpg|\.mpeg|\.tif|\.wav|\.mov|\.psd|\.ai|\.xls|\.mp4|\.m4a|\.swf|\.dat|\.dmg|\.iso|\.flv|\.m4v|\.torrent|\.ttf|\.woff))(.*) http://nextp-vm2b.vital-it.ch:8082/https://www.nextprot.org%{REQUEST_URI} [P]

```