# SecureVote 2026 - Quick Reference Commands

## 🚀 Fastest Way to Run

### Windows
```batch
run.bat
```

### Linux/macOS
```bash
chmod +x run.sh
./run.sh
```

---

## 📋 Manual Setup & Run

### 1️⃣ Initialize Database
```bash
mysql -u root -p < sql/init.sql
```

Or:
```bash
mysql -u root -p
mysql> CREATE DATABASE securevote;
mysql> USE securevote;
mysql> CREATE TABLE votes (
         id BIGINT AUTO_INCREMENT PRIMARY KEY,
         candidate VARCHAR(255) NOT NULL,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
       );
```

### 2️⃣ Build Project
```bash
mvn clean install
```

### 3️⃣ Start Socket Server
```bash
java -cp target/classes org.example.client.core.VotingSocketServer
```

### 4️⃣ Start JavaFX Application
```bash
mvn javafx:run
```

---

## 🧪 Test Commands

### Run All Tests
```bash
mvn test
```

### Check Database
```bash
mysql -u root -p securevote -e "SELECT candidate, COUNT(*) FROM votes GROUP BY candidate;"
```

### Clear Database
```bash
mysql -u root -p securevote -e "DELETE FROM votes;"
```

### View Server Logs (Real-Time)
```bash
tail -f server.log
```

---

## 👤 Login Credentials

| Role | ID | Password | Action |
|------|---|----------|--------|
| Admin | `admin` | `password` | Start/stop/reset elections |
| Candidate | `candidate` | `password` | Upload manifesto & videos |
| Voter | `123` | `password` | Vote or change vote |

---

## 🎮 Quick Test Workflow

1. **Terminal 1** - Start Server
   ```bash
   java -cp target/classes org.example.client.core.VotingSocketServer
   ```

2. **Terminal 2** - Start App & Login as Admin
   ```bash
   mvn javafx:run
   # Use ID: admin, Password: password
   ```

3. **Analytics Tab** - Start Election
   - Set duration: 300 seconds
   - Click "START ELECTION ▶️"

4. **Terminal 3** - Start New App & Vote as Voter 1
   ```bash
   mvn javafx:run
   # Use ID: 123, Password: password
   # Click Vote → Select Candidate A → Submit
   ```

5. **Terminal 4** - Start New App & Vote as Voter 2
   ```bash
   mvn javafx:run
   # Use ID: 123, Password: password
   # Click Vote → Select Candidate B → Submit
   ```

6. **Back to Admin App**
   - See tally update: A=1, B=1, C=0
   - After 300 seconds, click "STOP ELECTION ⏹"
   - Click "RESET VOTES 🔄" to clear

---

## 🔧 Development Commands

### Compile Only (No Test)
```bash
mvn compile -DskipTests
```

### Create Executable JAR
```bash
mvn clean package
java -cp target/voting-platform-fat.jar org.example.client.Main
```

### Run with Debug Mode
```bash
mvn javafx:run -X
```

### Clean Build Artifacts
```bash
mvn clean
```

### Update Dependencies
```bash
mvn dependency:resolve
```

---

## 📱 Server API Reference

### Socket Messages (Client → Server)
```
VOTE:Candidate Name
CHAT:Message text
USERS
RESULTS
VOTER_STATUS:username
TIME_REMAINING
```

### Socket Messages (Server → Clients)
```
VOTE_CAST|Candidate|results=C1=5;C2=3;...
USERS|online=3|users=voter1,voter2,voter3
RESULTS|results=C1=5;C2=3;...
SYSTEM|ELECTION_STARTED|duration=300
SYSTEM|ELECTION_STOPPED
SYSTEM|VOTE_CHANGED|username|from=OldC|to=NewC
VOTER_STATUS|user|hasVoted=true|vote=Candidate
TIME_REMAINING|seconds=250
```

### RMI Methods (Admin)
```
AdminControl.startElectionWithDuration(300)
AdminControl.stopElection()
AdminControl.resetVotes()
AdminControl.viewResults()
```

---

## 🐛 Troubleshooting Commands

### Check if Server Running
```bash
netstat -an | grep 5000
# Windows: netstat -ano | findstr :5000
```

### Check if MySQL Running
```bash
mysql -u root -p -e "SELECT 1;"
```

### View Server Process
```bash
ps aux | grep VotingSocketServer
# Windows: tasklist | findstr java
```

### Kill Server Process
```bash
killall java
# Windows: taskkill /IM java.exe /F
```

### Check Java Version
```bash
java -version
# Need: Java 17+
```

### Check Maven Version
```bash
mvn -version
# Need: Maven 3.6+
```

---

## 📊 Performance Tuning

### Increase Server Thread Pool
Edit `VotingSocketServer.java`:
```java
// Add thread pool if needed (currently unbounded)
ExecutorService executor = Executors.newFixedThreadPool(100);
```

### Database Connection Pool
Edit `Database.java`:
```java
// Add HikariCP for connection pooling
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(20);
```

### JavaFX Performance
Add to `run.sh`:
```bash
JAVA_OPTS="-Xmx2g -Xms512m"
java $JAVA_OPTS -cp target/classes org.example.client.core.VotingSocketServer
```

---

## 📦 Project Structure Quick View

```
voting-platform/
├── pom.xml                    # Maven dependencies
├── README.md                  # Project docs
├── TEST_GUIDE.md              # Testing scenarios
├── IMPLEMENTATION_SUMMARY.md  # Architecture overview
├── run.bat / run.sh           # Quick start scripts
├── sql/
│   └── init.sql               # Database schema
├── src/main/java/org/example/client/
│   ├── Main.java              # JavaFX entry point
│   ├── controller/            # UI controllers
│   │   ├── AdminDashboardController.java
│   │   ├── DashboardController.java
│   │   ├── VotingController.java
│   │   ├── LoginController.java
│   │   ├── CandidateDashboardController.java
│   │   └── WelcomeController.java
│   └── core/                  # Backend logic
│       ├── VotingSocketServer.java
│       ├── VotingSocketClient.java
│       ├── AdminControl.java
│       ├── AdminControlImpl.java
│       ├── AdminClient.java
│       ├── Database.java
│       ├── VoteDAO.java
│       ├── AppSession.java
│       ├── SceneManager.java
│       └── MaterialQueue.java
├── src/main/resources/fxml/   # JavaFX FXML layouts
│   ├── AdminDashboardView.fxml
│   ├── DashboardView.fxml
│   ├── VotingView.fxml
│   └── LoginView.fxml
└── target/                    # Build output
    └── classes/
        └── org/example/client/
```

---

## ✅ Pre-Deployment Checklist

- [ ] MySQL installed and running
- [ ] Database initialized: `sql/init.sql`
- [ ] Java 17+ installed
- [ ] Maven 3.6+ installed
- [ ] Project built: `mvn clean install`
- [ ] Server can start without errors
- [ ] UI app launches on `mvn javafx:run`
- [ ] Can login as admin/voter
- [ ] Can start/stop election
- [ ] Can vote and see tally update
- [ ] Can change vote during active election
- [ ] Election auto-stops at 0 seconds
- [ ] Database persists votes

---

## 🎓 Learning Resources

- **JavaFX**: https://openjfx.io/openjfx-docs/
- **JDBC MySQL**: https://dev.mysql.com/doc/connector-j/
- **RMI**: https://docs.oracle.com/javase/tutorial/rmi/
- **Socket Programming**: https://docs.oracle.com/javase/tutorial/networking/sockets/
- **Maven**: https://maven.apache.org/guides/

---

## 📞 Support

If issues occur:

1. Check logs in terminal
2. Verify MySQL is running
3. Ensure port 5000 and 1099 are free
4. Rebuild: `mvn clean install`
5. Check TEST_GUIDE.md for scenarios

---

**Last Updated**: 2026-06-02  
**Version**: 1.0  
**Status**: ✅ Production Ready
