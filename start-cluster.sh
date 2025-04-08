#!/bin/bash
# Использование: ./starter-cluster.sh [количество storage узлов]

set -e  # Выход при первой ошибке

STORAGE_COUNT=${1:-1}
MASTER_NODE_URL="http://localhost:9090"

register_storage_node() {
    local address=$1
    local json_data="{\"address\":\"$address\"}"

    echo "Регистрируем storage node: $address"
    response=$(curl -s -o /dev/null -w "%{http_code}" \
              -X POST "$MASTER_NODE_URL/scheme" \
              -H "Content-Type: application/json" \
              -d "$json_data")

    if [ "$response" -ne 200 ]; then
        echo "Ошибка регистрации storage node (код $response)"
        exit 1
    fi
    echo "Успешно зарегистрирован storage node: $address"
}

# Очистка
echo "Остановка предыдущих контейнеров..."
docker-compose down -v

# Запуск master-node
echo -e "\nЗапуск master-node и клиента"
docker-compose up -d master-node sharding-client

# Запуск storage узлов
echo -e "\nStarting $STORAGE_COUNT storage nodes..."
docker-compose up -d --scale sharding-storage=$STORAGE_COUNT

sleep 10

# Добавление storage узлов
echo -e "\nДобавление узлов в кластер..."
# Регистрация storage узлов по именам контейнеров
echo "Регистрация storage узлов в master-node..."
for i in $(seq 1 $STORAGE_COUNT); do
    register_storage_node "sharding-storage-sharding-storage-$i:8080"
done
echo -e "\nМожете подключиться к клиенту"