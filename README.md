# noedap

Because your brain is bad at math and your phone's calculator won't remember yesterday's hours.

**noedap** is a simple work hour calculator for flexible schedules. Input your start and end times, it calculates your net hours (with breaks), and shows how today affects your weekly total. No live tracking, no timer - just straightforward number crunching.

## Features

- **Daily calculation** - Input start and end, get your net hours
- **Automatic break deduction** - Configurable breaks after X hours
- **Weekly tracking** - See current week's total and remaining hours
- **Day navigation** - Jump between days, see the impact
- **History** - Review past days and weeks
- **Fully customizable** - Set your weekly target, core hours per day, and break rules
- **Clean & simple** - No timer, no live tracking, just a calculator that remembers

## How it works

1. Open the app
2. Pick the date
3. Enter when you started and when you left (or plan to leave)
4. See your net hours for the day
5. Watch how it affects your weekly progress
6. Figure out when you can actually leave without working overtime

It's a spreadsheet that doesn't require Excel skills. Set your target hours in Settings, then just plug in your times.

## Vibecoded

This project was built by [Mistral Vibe](https://vibe.mistral.ai) - an AI coding agent that's surprisingly good at Kotlin and bad at UX design. The architecture is clean, the commits are atomic, and no, it won't start a timer on you.

## Tech Stack

- **Kotlin** - Because Java is for people who enjoy pain
- **AndroidX** - The modern way to do Android
- **Room** - For persistence that doesn't suck
- **DataStore** - For preferences without the SharedPreferences mess
- **Coroutines & Flow** - Because callbacks are so 2015
- **Material Design 3** - So it doesn't look like it was designed in 2010

## Installation

Clone, open in Android Studio, build, run. You know the drill.

## License

Do whatever you want with it. It's your calculator.
