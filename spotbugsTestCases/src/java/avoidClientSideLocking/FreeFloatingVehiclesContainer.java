package avoidClientSideLocking;

public class FreeFloatingVehiclesContainer {
    private Object availableFFVehicleLocationQuadTree; 
    private Object network; 

    public FreeFloatingVehiclesContainer(Object availableFFVehicleLocationQuadTree) {
        this.availableFFVehicleLocationQuadTree = availableFFVehicleLocationQuadTree;
    }

    public void setNetwork(Object network) {
        this.network = network;
    }

    public boolean reserveVehicle(Object vehicle) {
        synchronized (availableFFVehicleLocationQuadTree) {
            System.out.println("Reserving vehicle: " + vehicle);
            return true;
        }
    }

    public void parkVehicle(Object vehicle, Object link) {
        synchronized (availableFFVehicleLocationQuadTree) {
            System.out.println("Parking vehicle: " + vehicle);
        }
    }

    public Object getFfVehicleLocationQuadTree() {
		return availableFFVehicleLocationQuadTree;
	}

    public static void main(String[] args) {
        Object quadTree = new Object();
        FreeFloatingVehiclesContainer container = new FreeFloatingVehiclesContainer(quadTree);
        Object vehicle = new Object();
        Object link = new Object();
        Object network = new Object();

        container.setNetwork(network);

        container.reserveVehicle(vehicle);
        container.parkVehicle(vehicle, link);
    }
}

