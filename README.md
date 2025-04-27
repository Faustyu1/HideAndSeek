# 🎮 Hide and Seek

A Minecraft plugin that adds a classic hide and seek game to your server. Seekers (jury) must find all hiding players before time runs out.

## ✨ Features
- Two teams: Hiders and Seekers
- Configurable hiding and seeking phases
- Special effects and player glowing
- Game statistics at the end
- Seeker's stick for finding players
- Team-based gameplay with color identification
- Customizable messages and translations

## 🎯 Commands
- `/hideandseek start` - Start the game
- `/hideandseek sethidersspawn` - Set hiders spawn point
- `/hideandseek setseekersspawn` - Set seekers spawn point

## ⚙️ Configuration
```yaml
# config.yml
settings:
  hide-time: 30    # Hiding phase duration (seconds)
  seek-time: 300   # Seeking phase duration (seconds)
  glow-time: 30    # Time before players start glowing (seconds)
```

```yaml
# language.yml
messages:
  game:
    start: "⚔ The game will start in %seconds% seconds!"
    # ... more messages
  player:
    found: "%seeker% found %player%!"
    # ... more messages
```

## 🔧 Requirements
- Minecraft 1.16+
- Spigot/Paper server
- Java 8 or higher
- Gradle 7.0+

## 🛠️ Building
1. Clone the repository
```bash
git clone https://github.com/yourusername/HideAndSeek.git
cd HideAndSeek
```

2. Build with Gradle
```bash
gradle build
```

The compiled JAR will be in `build/libs/` directory.

## 📥 Installation
1. Copy the JAR file to your server's `plugins` folder
2. Restart the server
3. Set spawn points using the commands above
4. Customize messages in `language.yml` if needed

## 🤝 Contributing
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details 