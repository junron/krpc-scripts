suspend fun main(args: Array<String>) {
    when(args[0]){
        "deorbit" -> Deorbit.main(args)
        "launch" -> Launch.main(args)
        "heavy-launch" -> HeavyLaunch.main(args)
        "land" -> Land.main(args)
        "dock" -> Docking.main(args)
        "sync" -> Synchronous.main(args)
    }
}
