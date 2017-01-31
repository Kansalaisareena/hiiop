if [ ! -e ~/leiningen/bin/lein ]; then
    cd ~/
   git clone -b stable --single-branch https://github.com/technomancy/leiningen.git
   cd ~/leiningen/leiningen-core && lein bootstrap
fi
