package net.paulem.launchermc.gson.types;

public record MinetoolsServer(String description, String favicon, double latency, MinetoolsPlayers players, MinetoolsVersion version) {

    public record MinetoolsPlayers(int max, int online, MinetoolsSample[] sample){

        public record MinetoolsSample(String id, String name){
        }

    }

    public record MinetoolsVersion(String name, Object protocol){
    }

}
