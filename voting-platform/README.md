# SecureVote 2026 - Complete Voting Platform

A full-featured JavaFX voting application with socket-based real-time updates, RMI remote admin control, and MySQL persistence.

## Architecture

**Person 1 - Socket Server & Multithreading (Backend)**
- `VotingSocketServer.java` - Multi-threaded socket server handling concurrent voters
- Thread pool architecture: one thread per connected client
- In-memory vote tracking + persistent database storage
- Live vote broadcasting to all connected clients
- Voter duplicate detection and change-vote support

**Person 2 - Database & Persistence (Data Layer)**
- `Database.java` - JDBC connection helper for MySQL
- `VoteDAO.java` - Data access object for vote persistence
- SQL schema: `votes` table with candidate names and timestamps
- Vote recording, querying, and reset operations

**Person 3 - UI & RMI Admin Control (Frontend)**
- JavaFX UI for voters, candidates, and admins
- `AdminControl.java` / `AdminControlImpl.java` - RMI interface for remote election management
- `AdminClient.java` - RMI client for admin dashboard
- Election timer with countdown display
- Vote change feature during active election

## Features Implemented

✅ **Multi-client voting** - Multiple voters can connect and vote simultaneously
✅ **Live updates** - Socket-based message broadcasting to all clients
✅ **Vote persistence** - All votes stored in MySQL database
✅ **Duplicate vote detection** - Voters cannot double-vote (can change if election active)
✅ **Election timer** - Countdown starts when admin triggers election start
✅ **RMI admin control** - Remote start/stop/reset election without server access
✅ **User roles** - Admin, Candidate, Voter with role-based UI
✅ **Real-time UI** - Dashboard shows live server messages and time remaining
✅ **Smooth transitions** - Scene transitions with proper error handling

## Installation

### 1. Setup MySQL Database

```bash
mysql -u root -p < sql/init.sql
```

Or manually:
```sql
CREATE DATABASE IF NOT EXISTS securevote CHARACTER SET utf8mb4;
USE securevote;

CREATE TABLE IF NOT EXISTS votes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  candidate VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2. Configure Database Credentials

Edit `src/main/java/org/example/client/core/Database.java`:
```java
private static final String HOST = "localhost";
private static final String PORT = "3306";
private static final String DB_NAME = "securevote";
private static final String USER = "root";
private static final String PASSWORD = "password";  // Change to your password
```

### 3. Build Project

```bash
mvn clean install
```

## Running the Platform

### Start the Socket Server (Terminal 1)

```bash
java -cp target/classes org.example.client.core.VotingSocketServer
```

Expected output:
```
=================================
SecureVote socket server starting
Port: 5000
=================================
RMI AdminControl bound on port 1099
```

### Start the JavaFX Application (Terminal 2)

```bash
mvn javafx:run
```

Or from IDE: Right-click `Main.java` → Run

## Usage

### Admin User
- **Login**: ID `admin` / Password `password`
- **Controls**:
  - Set election duration (in seconds)
  - Click "Start Election" - countdown begins
  - Click "Stop Election" - voting halts
  - Click "Reset Votes" - clear all votes and voter tracking
  - View live vote tallies in Analytics view
  - Manage candidate approvals and material uploads

### Candidate User
- **Login**: ID `candidate` / Password `password`
- **Actions**:
  - Upload PDF manifesto and MP4 campaign video
  - Submit materials for admin approval
  - View approval status

### Voter User
- **Login**: ID `123` / Password `password`
- **Actions**:
  - View candidate list and campaign materials
  - Click "Vote" to enter voting booth
  - Select a candidate and submit
  - **If election is active**: Can change vote
  - **If election is closed**: Cannot vote
  - See message: "✅ You have already voted. Select to change your vote or cancel."

## Code Structure

```
src/main/java/org/example/client/
├── Main.java                           # JavaFX entry point
├── controller/
│   ├── AdminDashboardController.java   # Admin UI + RMI client
│   ├── DashboardController.java        # Voter dashboard with timer
│   ├── VotingController.java           # Vote submission + change vote
│   ├── LoginController.java            # Authentication + session init
│   ├── CandidateDashboardController.java
│   └── WelcomeController.java
├── core/
│   ├── VotingSocketServer.java         # Main socket server
│   ├── VotingSocketClient.java         # Client API for voters
│   ├── AdminControl.java               # RMI interface
│   ├── AdminControlImpl.java            # RMI implementation
│   ├── AdminClient.java                # RMI client for admin
│   ├── Database.java                   # JDBC connection helper
│   ├── VoteDAO.java                    # Data access object
│   ├── AppSession.java                 # Session state (username, role)
│   └── SceneManager.java               # UI navigation
└── resources/fxml/                     # JavaFX FXML layouts
```

## Server Messages (Socket Protocol)

```
VOTE_CAST|Candidate Name|results=Cand1=5;Cand2=3;...
USERS|online=3|users=voter1,voter2,voter3
RESULTS|results=Cand1=5;Cand2=3;...
SYSTEM|ELECTION_STARTED|duration=600
SYSTEM|ELECTION_STOPPED
SYSTEM|VOTE_CHANGED|username|from=OldCand|to=NewCand
VOTER_STATUS|username|hasVoted=true|vote=CandidateName
TIME_REMAINING|seconds=425
```

## Testing End-to-End

1. **Start server** (Terminal 1)
2. **Start app** (Terminal 2)
3. **Login as admin** → Start election (300 seconds)
4. **Open 2 more app windows** (or new terminals)
5. **Login as voter_1** → Vote for Candidate A
6. **Login as voter_2** → Vote for Candidate B
7. **Verify on admin**: Tally updates in real-time
8. **Try changing vote as voter_1**: Should update tally
9. **Wait for timer** → Election auto-stops at 0s
10. **Try voting as new voter_3** → Should be rejected

## Security Features

- Role-based access control (ADMIN, VOTER, CANDIDATE)
- Duplicate vote prevention with voter tracking
- Vote change only during active election
- RMI bound to localhost only (modify for network deployment)
- Session-based user context (AppSession)

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Connection refused" | Ensure socket server is running on port 5000 |
| "RMI connection failed" | Socket server must be started first (RMI registry created on startup) |
| "Database connection failed" | Check MySQL credentials in `Database.java` and verify DB exists |
| "FXML not found" | Rebuild with `mvn clean install` to copy resources to target/ |
| "JavaFX module not found" | Run with `mvn javafx:run` or configure IDE with JavaFX SDK path |

## Future Enhancements

- [ ] Biometric voter authentication
- [ ] End-to-end encryption for vote transmission
- [ ] Distributed ledger (blockchain) vote logging
- [ ] Multi-region server deployment with replication
- [ ] Audit trail and vote verification receipts
- [ ] Advanced analytics and polling predictions

## License

Proprietary - SecureVote 2026 Election Platform
