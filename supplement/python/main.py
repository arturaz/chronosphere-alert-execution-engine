from client import Client

def main():
    client = Client("")

    # Retrieving alerts.
    alerts = client.query_alerts()
    print(alerts)

    # Querying a metric.
    resp = client.query("test-query-1")
    print(resp)

    # Triggering an alert.
    client.notify("alert-1", "test-message")

    # Resolving an alert.
    client.resolve("alert-1")

if __name__ == "__main__":
    main()
