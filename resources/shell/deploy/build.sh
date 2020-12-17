ssh -t root@"$1" "
    [[ -d /home/$2 ]]               || mkdir /home/$2;
"
scp -rq ./docker-compose.yml root@"$1":/home/$2;

ssh -t root@"$1" "
    cd /home/$2
    docker-compose pull && docker-compose up -d
"