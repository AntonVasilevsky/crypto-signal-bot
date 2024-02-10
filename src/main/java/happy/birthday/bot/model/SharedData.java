package happy.birthday.bot.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
@Getter
public class SharedData {
    private final Lock userSignalsLock;
    private final Lock extractedObjectsLock;
    private final Lock matchedSignalsLock;


    //  private List<JsonObject> extractedObjects = Collections.synchronizedList(new ArrayList<>());
    private List<JsonObject> extractedObjects;
    private final List<Signal> userSignals;
    public List<Signal> matchedSignals;

    public SharedData(Lock userSignalsLock, Lock extractedObjectsLock, Lock matchedSignalsLock) {
        this.userSignalsLock = userSignalsLock;
        this.extractedObjectsLock = extractedObjectsLock;
        this.matchedSignalsLock = matchedSignalsLock;
        extractedObjects = new ArrayList<>();
        userSignals = new ArrayList<>();
        matchedSignals = new ArrayList<>();
    }

    public void addUserSignalsWithLock(Signal signal) {
        try {
            userSignalsLock.lock();
            userSignals.add(signal);
        } finally {
            userSignalsLock.unlock();
        }

    }
    public int getUserSignalsSizeWithLock() {
        try {
            userSignalsLock.lock();
            return userSignals.size();
        } finally {
            userSignalsLock.unlock();
        }
    }
    public List<Signal> getUserSignalsCopyWithLock() {
        try {
            userSignalsLock.lock();
            return new ArrayList<>(userSignals);
        } finally {
            userSignalsLock.unlock();
        }
    }
    public Signal getUserSignalsIndexWithLock(int index) {
        try {
            userSignalsLock.lock();
            return userSignals.get(index);
        } finally {
            userSignalsLock.unlock();
        }
    }

    public boolean removeFromUserSignalsWithLock(Signal matchedSignal) {
        boolean success;
        try {
            userSignalsLock.lock();
            success = userSignals.remove(matchedSignal);
        } finally {
            userSignalsLock.unlock();
        }
        return success;
    }
    public Signal removeFromUserSignalsWithLock(int index) {
        Signal signal;
        try {
            userSignalsLock.lock();
            signal = userSignals.remove(index);
        } finally {
            userSignalsLock.unlock();
        }
        return signal;
    }

    public void cancelAllOrders() {
        try {
            userSignalsLock.lock();
            userSignals.clear();
        } finally {
            userSignalsLock.unlock();
        }
    }

    public List<JsonObject> getExtractedObjectsCopyWithLock() {
        try {
            extractedObjectsLock.lock();
            return new ArrayList<>(extractedObjects);
        } finally {
            userSignalsLock.unlock();
        }
    }

    public void setExtractedObjectsWithLock(List<JsonObject> jsonObjectListFromApi) {
        try {
            extractedObjectsLock.lock();
            extractedObjects = jsonObjectListFromApi;
        } finally {
            userSignalsLock.unlock();
        }
    }

    public List<Signal> getMatchedSignalsCopyWithLock() {
        try {
            matchedSignalsLock.lock();
            return new ArrayList<>(matchedSignals);
        } finally {
            matchedSignalsLock.unlock();
        }
    }

    public boolean matchedSignalsIsEmpty() {
        try {
            matchedSignalsLock.lock();
            return matchedSignals.isEmpty();
        } finally {
            matchedSignalsLock.unlock();
        }
    }

    public Signal removeFromMatchedSignals(int i) {
        try {
            matchedSignalsLock.lock();
            return matchedSignals.remove(i);
        } finally {
            matchedSignalsLock.unlock();
        }
    }
    public boolean removeFromMatchedSignals(Signal signal) {
        try {
            matchedSignalsLock.lock();
            return matchedSignals.remove(signal);
        } finally {
            matchedSignalsLock.unlock();
        }
    }

    public void addMatchedSignalsWithLock(Signal s) {
        try {
            matchedSignalsLock.lock();
            matchedSignals.add(s);
        } finally {
            matchedSignalsLock.unlock();
        }
    }
}
