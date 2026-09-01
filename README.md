# Comets

Comets is an Asteroids-inspired arcade game built with Java Swing.

The game features real-time ship movement, projectile mechanics, collision detection, destructible comets, scoring, lives, persistent high scores, and a hyperspace mechanic.

## Features

- Real-time ship movement with acceleration, friction, and rotation
- Screen wrapping
- Projectile firing with cooldown timing
- Comets that break into smaller fragments when destroyed
- Bullet-to-comet collision detection
- Ship-to-comet collision detection
- Score and lives system
- Persistent high-score saving
- Game-over and restart functionality
- Hyperspace teleport mechanic
- Approximately 60 FPS game loop
- Custom rendering using Java Swing and Java2D

## Controls

| Key | Action |
|---|---|
| W / Up Arrow | Thrust |
| A / Left Arrow | Rotate Left |
| D / Right Arrow | Rotate Right |
| Space | Fire |
| E | Hyperspace Jump |
| R | Restart after Game Over |

## Technical Concepts

This project demonstrates several Java and object-oriented programming concepts, including:

- Object-oriented inheritance
- Encapsulation
- Java Swing GUI development
- Java2D graphics
- Event-driven keyboard input
- Multithreading
- Real-time game loops
- Collision detection
- Vector-based movement
- File I/O
- Collection management
- Iterator-based object removal

The game's objects share a common `GameObject` base class, while specialized classes such as `Ship`, `Bullet`, and `Comet` implement their own behavior.

## Project Structure

```text
comets-java-game/
├── src/
│   └── Comets.java
├── screenshots/
├── .gitignore
└── README.md
