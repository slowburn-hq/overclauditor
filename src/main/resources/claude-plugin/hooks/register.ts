import type { Register } from 'claude-code'

// The IDE's model dropdown and its session naming, run INSIDE the session: typed into the
// terminal, /model drew its picker over the prompt box and /rename stashed the user's draft.
// The IDE writes one request at a time to $CLAUDITOR_STATUS_FILE.cmd ({ seq, kind, value });
// the answer goes to .cmd.done. `.hook` tells the IDE this route is live (an older CLI without
// mods never writes it, and the IDE keeps typing). Setting `model` also saves it as the user's
// default; the IDE puts the old default back once the answer lands.
export const register: Register = on => {
  on('session.start', async ($, e, next) => {
    const r = await next(e)
    const base = await $.env.get('CLAUDITOR_STATUS_FILE')
    if (!base) return r
    const cmd = `${base}.cmd`
    let seen: unknown = null
    let busy = false
    $.clock.every(250, async () => {
      if (busy || !(await $.fs.exists(cmd))) return
      busy = true
      try {
        const req = JSON.parse(await $.fs.read(cmd)) as { seq: unknown; kind: string; value: string }
        if (req.seq === seen) return
        seen = req.seq
        let ok = false
        let error = ''
        try {
          if (req.kind === 'model') {
            const { deny } = await $.config.set({ key: 'model', value: req.value })
            ok = !deny
            if (deny) error = String(deny)
          } else if (req.kind === 'rename') {
            await $.command.run({ command: 'rename', args: req.value })
            ok = true
          } else error = `unknown kind ${req.kind}`
        } catch (err) {
          error = String(err)
        }
        await $.fs.write(`${cmd}.done`, JSON.stringify({ seq: req.seq, ok, error }))
      } catch {
        // a half-written request: read again next tick
      } finally {
        busy = false
      }
    })
    await $.fs.write(`${base}.hook`, '1')
    return r
  })
}
