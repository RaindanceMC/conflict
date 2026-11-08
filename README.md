# RaindanceMC/conflict

PvP and base raiding, opt-in per player (module `conflict`, plugin `RainsConflict`).

- `/pvp [on|off|status]` — two players can hurt each other only when both have PvP on.
- `/raid [on|off|status]` — raiders may build, break, open containers and doors and blow up TNT in the claims of
  other raiders (every owner of the claim must be a raider). Raiding includes PvP.
- Switching on is instant; switching off takes effect after a delay (settings `conflict.pvp-off-minutes`,
  `conflict.raid-off-hours`) so nobody leaves a fight or a raid by typing.
- Off until `conflict.enabled` is switched on in `/settings`; with it off the server's own PvP rules apply.
