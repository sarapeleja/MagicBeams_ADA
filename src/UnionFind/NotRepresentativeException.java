package UnionFind;

public class NotRepresentativeException extends RuntimeException {

    static final long serialVersionUID = 0L;


    public NotRepresentativeException( ) {
        super();
    }

    public NotRepresentativeException( String message ) {
        super(message);
    }

}

