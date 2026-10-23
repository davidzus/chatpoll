# chatpoll

A server-side [Fabric](https://fabricmc.net/) mod for Minecraft that lets you create polls and vote on them right in chat.

Create a poll, add options, start it, and players vote with a single command. Results are shown as a color-coded summary in chat, with vote counts, percentages and a progress bar for each option. You can send the results only to yourself or broadcast them to the whole server.

## Commands

Poll names that contain spaces have to be wrapped in quotes, for example `"Next map"`. Options can contain spaces without quotes because they are always the last argument.

Every command supports tab completion for poll names and options.

### Managing polls

- The `/poll` commands need Operator permissions (OP Level 2).

| Command | Description |
|---------|-------------|
| `/poll create <name>` | Create a new, empty poll. |
| `/poll add <name> <option>` | Add an option to a poll. You can only add options before the poll is started. |
| `/poll start <name>` | Open the poll for voting. The poll needs at least one option, and a poll that has ended can't be started again. |
| `/poll end <name>` | Close the poll and show the final results. |
| `/poll delete <name>` | Delete a poll. A running poll has to be ended first. |
| `/poll list` | List all polls and their status: `not started`, `active` or `ended`. |
| `/poll results <name> [broadcast]` | Show the current results. Pass `true` as `broadcast` to send them to every player on the server instead of only to you. |

### Voting

| Command | Description |
|---------|-------------|
| `/vote <name> <option>` | Vote for an option in an active poll. Each player can vote once per poll. |

### Example

```
/poll create "Next map"
/poll add "Next map" Desert Temple
/poll add "Next map" Ocean Monument
/poll add "Next map" Woodland Mansion
/poll start "Next map"

/vote "Next map" Ocean Monument

/poll results "Next map" true
/poll end "Next map"
```

## Limitations

The voting system is still a work in progress:

- Polls are only kept in memory and are lost when the server restarts.

## License

This project is licensed under the [MIT License](LICENSE).
