#!/bin/sh
# Repite las pruebas de concurrencia (doble reserva y rotación de refresh) para comprobar que no son intermitentes.
# Uso, desde la raíz del workspace:  docker compose exec -T citas-api-dev sh scripts/repeat-concurrency.sh [veces]
RUNS=${1:-5}
TESTS='BookingIntegrationTest#hu022_ca02_concurrentBookingsOfTheSameSlotProduceExactlyOneAppointment,SessionLifecycleIntegrationTest#ca04_concurrentRefreshWithTheSameTokenAllowsExactlyOneRotation'
failed=0
i=1
while [ "$i" -le "$RUNS" ]; do
  mvn -q -B test -Dtest="$TESTS" > "/tmp/concurrency-$i.log" 2>&1
  status=$?
  failures=$(grep -c '<<< FAIL' "/tmp/concurrency-$i.log")
  echo "corrida $i: exit=$status fallos=$failures"
  [ "$status" -ne 0 ] && failed=1
  i=$((i + 1))
done
exit $failed
