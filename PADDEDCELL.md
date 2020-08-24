# Deprecation notice

Deprecation notice: `paddedCell()` has ben deprecated since Autostyle 3.2,
so if you observe `padded cell` issue, you might want to upgrade Autostyle.

This is not a bug in Autostyle itself, but in one of the third-party formatters, such as the [eclipse formatter](https://bugs.eclipse.org/bugs/show_bug.cgi?id=310642), [google-java-format](https://github.com/google/google-java-format/issues), or some custom rule.

Autostyle will try its best to ensure that your code continues to be formatted,
although it will be a little slower.  Now when you run `autostyleCheck`,
it will generate helpful bug report files in the `build/autostyle-diagnose-<FORMAT_NAME>`
folder which will contain the states that your rules are fighting over.

These files are very helpful to the developers of the code formatter you are using.

## How Autostyle works

Autostyle works on a very simple principle

- You specify a series of steps (`trimTrailingWhitespace()`, `licenseHeader('/* Licensed under Apache-2.0 */')`, etc.)
- Each step is a `Function<String, String>` - takes a `String` as input, returns a `String` as output

When you call
- `autostyleApply`, it reads a file, applies each step sequentially, then writes the output back to disk.
- `autostyleCheck`, it reads a file, applies each step sequentially, and makes sure that the output of the steps is equal to the input.  If not, it tells you which files are badly formatted, and asks you to run `autostyleApply` to fix them.

## A misbehaving step

Let's imagine that we wrote a step like this:

```groovy
custom 'pingpong', { input ->
  if (input.equals('A')) {
    return 'B'
  } else {
    return 'A'
  }
}
```

If our input file is `CCCCCC`, then the first time we call `autostyleApply` we'll get `A`, the next time `B`, the next time `A`, back and forth.  This misbehaving rule is self-inconsistent - it doesn't know what it wants the format to be.  Because of this, `autostyleCheck` will always fail.

The rule we wrote above is obviously a bad idea.  But complex code formatters can have corner-cases where they exhibit exactly this behavior of ping-ponging between two states.  It's also possible to have a cycle of more than two states.

Formally, a correct formatter `F` must satisfy `F(F(input)) == F(input)` for all values of input.  Any formatter which doesn't meet this rule is misbehaving.

# What if the formatter does not converge?

In order to check if the formatting converges, Autostyle tries to call the formatter multiple times:

- When you call `autostyleApply`, it will automatically check for a ping-pong condition.
- If there is a ping-pong condition, it will resolve the ambiguity arbitrarily, but consistently
- It will also warn that `filename such-and-such cycles between 2 steps`.

## How is the ambiguity resolved?

This is easiest to show in an example:

* Two-state cycle: `'CCCC' 'A' 'B' 'A' ...`
  + `F(F('CCC'))` should equal `F('CCC')`, but it didn't, so we iterate until we find a cycle.
  + In this case, that cycle turns out to be `'B' 'A'`
  + To resolve the ambiguity about which element in the cycle is "right", Autostyle uses the lowest element sorted by first by length then alphabetically.
  + In this case, `A` is the canonical form which will be used by `autostyleApply` and `autostyleCheck`.

* Four-state cycle: `'CCCC' 'A' 'B' 'C' 'D' 'A' 'B' 'C' 'D'`
  + As above, we detect a cycle, and it turns out to be `'B' 'C' 'D' 'A'`
  + We resolve this cycle with the lowest element, which is `A`

* Convergence: `'ATT' 'AT' 'A' 'A'`
  + `F(F('ATT'))` did not equal `F('ATT')`, but there is no cycle.
  + Eventually, the sequence converged on `A`.
  + As a result, we will use `A` as the canoncial format.

* Divergence: `'1' '12' '123' '1234' '12345' ...`
  + This format does not cycle or converge
  + As a result, the canonical format is whatever the starting value was, which is `1` in this case.
  + PaddedCell gives up looking for a cycle or convergence and calls a sequence divergent after 10 tries.
