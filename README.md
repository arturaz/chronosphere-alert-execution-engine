# How the engine works

All versions of the engine (in different programming languages) works the same way.

First the engine initializes:

    It queries the available alerts via the /alerts endpoint.
    It initializes per-alert engines for each of the alerts.

Each per-alert engine then does this:

    The initial state of the engine is Pass.
    The engine goes into a loop until canceled, doing:
        Query the value using /query endpoint, retrying with backoff schedule until the call succeeds.
        Compute the state of the alert from the query value. This can produce Pass/Warn/Critical state.
        Check if the computed state is different from the current state. If it is different, asynchronously launch a new per-alert reporter, aborting the old reporter if it exists.
        Wait for the alert intervalSecs until the next iteration.

The idea behind per-alert reporter is that it is a separate logical process that is decoupled from the querying process, as in reality, you probably would send the data to a different server.

Thus the process of reading the query value (which can fail and should be done on it's own schedule) is not really tied to a process of reporting the current state of an alert (which can also fail and should be done on it's own schedule), which this reporter implements.

Per-alert reporter, when launched, does:

    If the reported state is Warn/Crit:
        The reporter will try to call the /notify endpoint, until it succeeds.
        The reporter will sleep for the RepeatIntervalSec, as stated in the docs:

            RepeatIntervalSecs - how often should messages be re-sent for an alert that is violating thresholds (default 5m). 


        (By the way, I am not sure what we should do with the default. Does that indicate that the property might be missing and if it is not defined we should treat it as if it was 5 minutes?)
        It will go back to step 1.
    If the reported state is Pass:
        The reporter will try to call the /resolve endpoint, until it succeeds, then stop.

        The rationale behind that is that after the alert is in Pass state, the notion of 'keep telling me that we're in a Pass state' seems more like a heartbeat mechanism. And also, the RepeatIntervalSecs explicitly states that it is only for alerts that violate thresholds.
