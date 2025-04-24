#!/bin/bash
# Использование: ./starter-cluster.sh [количество storage узлов]

set -e  # Выход при первой ошибке

STORAGE_COUNT=${1:-1}
MASTER_NODE_URL="http://localhost:9090"
STORAGE_PORT_START=8085  # Начальный порт для storage узлов

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

# Генерация docker-compose.override.yml с отдельными сервисами для каждого storage
echo "Генерация конфигурации портов..."
cat > docker-compose.override.yml <<EOL
version: '3.8'

services:
EOL

for ((i=0; i<STORAGE_COUNT; i++)); do
    port=$((STORAGE_PORT_START + i))
    cat >> docker-compose.override.yml <<EOL
  sharding-storage-$i:
    image: l1zail/sharding-storage:latest
    ports:
      - "$port:8080"
    networks:
      - default

EOL
done

# Запуск master-node и клиента
echo -e "\nЗапуск master-node и клиента..."
docker-compose up -d master-node sharding-client

# Запуск storage узлов
echo -e "\nЗапуск $STORAGE_COUNT storage узлов..."
for ((i=0; i<STORAGE_COUNT; i++)); do
    echo "Запуск sharding-storage-$i..."
    docker-compose up -d sharding-storage-$i
done

# Подождать немного перед регистрацией
echo -e "\nОжидание запуска контейнеров..."
sleep 10

# Регистрация узлов
echo -e "\nРегистрация storage узлов в master-node..."
for ((i=0; i<STORAGE_COUNT; i++)); do
    port=$((STORAGE_PORT_START + i))
    register_storage_node "host.docker.internal:$port"
done

echo -e "\n✅ Готово! Можете подключиться к клиенту."