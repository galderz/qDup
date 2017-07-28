# Ssh
Coordinate multiple shells for running tests in a lab environment
## Introduction
Running benchmarks (e.g. specjms) requires opening several
terminals and monitoring the output of one terminal to start
a command in another terminal. This project provides a way to script
multiple shells and coordinate between them.

Each terminal connection starts by executing a sequence of commands
in a `Script`. All terminals start at the same time but `waitFor`,
`signal`, and `repeat-until` commands help coordinate the execution.

For example, the eap script would call `waitFor DATABASE_READY` and
the database script would call `signal DATABASE_READY` when it finished
loading. The database would then call `waitFor SERVER_STOPPED` before
stopping.
An example monitoring script that wants to collect jstack from the server
could call `waitFor SERVER_STARTED` and `repeat-until SERVER_STOPPING`
to only execute while the server is up. Just be sure there are not
conditions where `SERVER_STOPPING` would not be reached (e.g. error cases)

## Commands
* __abort: message__ - Abort the current run and log the message
* __code: `(input,state)->{...}`__ - execute a `Code` instance that returns `Result`
* __countdown: name initial__ - decrease a `name` counter which starts with `amount`.
Child commands will only be invoked each time after `name` counter reaches 0.
* __ctrlC:__ - send ctrl+C interrupt to the remote shell. This will kill
any currently running command (e.g. `sh tail -f /tmp/server.log`)
* __download: path ?destination__ - download `path` from the connected
host and save the output to the  run output path + `destination`
* __echo:__ - log the input to console
* __log: message__ - log `message` to the run log
* __read-state: name__ - read the current value of the named state variable
and pass it as input to the next command
* __regex: pattern__ - try to match the previous output to a regular
expression and add any named capture groups to the current state at
the named scope.
* __repeat-until: name__ - repeat the child commands until `name` is signalled.
Be sure to add a `sleep` to prevent a tight loop and pick a `name` that
is signalled in all runs (e.g. be careful of error conditions)
* __set-state name ?value__ set the named state attribute to `value`
if present or to the input if no value is provided
* __script: name__ - Invoke the `name` script as part of the current script
* __sh: command__ - Execute a remote shell command
* __signal: name__ - send one signal for "name." Runs parse all script :
host associations to calculate the expected number of `signal`s for each
`name`. All `waitFor` will wait for the expected number of `signal`
* __sleep: ms__ - pause the current script for the given number of milliseconds
* __queue-download: path ?destination__ - queue the download action for
after the run finishes. The download will occur if the run completes
or aborts
* __waitFor: name__ - pause the current script until "name" is fully signaled
* __xpath: path__ - This is an overloaded command that can perform an xpath
  based search or modification. Path takes the following forms
   - `file>xpath` - finds all xpath matches in file and passes them as
   input to the next command
   - `file>xpath == value` - set xpath matches to the specified value
   - `file>xpath ++ value` - add value to the xpath matches
   - `file>xpath --` - delete xpath matches from their parents
   - `file>xpath @key=value` - add the key=value attribute to xpath matches

## Watching
Some commands (e.g. sh commands) can provide updates during execution.
A child command with the `watch` prefix will receive each new line of
output from the parent command as they are available (instead of after
the parent completes). This is mostly used to monitor output with `regex`
and to subsequently call `signal STATE` or `ctrlC` the command when a
condition is met.
Note: `sh`, `waitFor`, and `repeat-until` cannot be used when watching
a command because they can block the exection of other watchers.

## State
State is the main way to introduce variability into a run. Commands can
reference state variables by name with `${{name}}` and `regex` can define
new entries with standard java regex capture groups `(?<name>.*)`


## YAML
The best way to use the tool is to run `gralde jar` and use the executable
jar to run tests based on yaml configuration.

## YAML support
 - [x] name
 - [x] scripts
   - [x] commands
   - [x] watchers
   - [x] with
 - [x] hosts
   - [x] shortname : user@host:port
   - [x] shortname : {user,host,port}
   - [ ] user@host:post ?
   - [ ] {user,host,port} ?
 - [ ] roles
   - [x] role names
   - [x] hosts by shortname
   - [ ] host declaration (e.g. `user@host:port`) in role:hosts
         ? use Provider<Host>
 - [ ] setup-scripts
   - [x] script name (applies to all
   - [ ] {select,script} to filer hosts
 - [ ] run-scripts
   - [ ] script name (applies to all
   - [ ] {select,script} to filer hosts
 - [ ] cleanup-scripts
   - [ ] script name (applies to all
   - [ ] {select,script} to filer hosts
 - [ ] state
   - [ ] run
   - [ ] host
   - [ ] script
 - [ ] coordinator
   - [ ] latches
   - [ ] counters