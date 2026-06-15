# Delta for Crypto Module

## MODIFIED Requirements

### Requirement: Session Management (REQ-CRYPTO-06)

The system MUST hold the VEK and DBK in ByteArrays in memory only — never persisted. closeSession MUST zero-fill both arrays before setting them to null.
(Previously: Only VEK was managed. DBK was returned from auth but unused.)

#### Scenario: Open and retrieve session keys
- GIVEN a 256-bit VEK and a 256-bit DBK
- WHEN openSession(VEK, DBK) is called
- THEN getVek() returns the VEK and getDbk() returns the DBK

#### Scenario: Close session zero-fills both keys
- GIVEN an open session with VEK and DBK stored
- WHEN closeSession is called
- THEN both ByteArrays are filled with zeros
- AND getVek() and getDbk() return null

#### Scenario: Close with no active session is safe
- GIVEN no session is open
- WHEN closeSession is called
- THEN no exception is thrown
