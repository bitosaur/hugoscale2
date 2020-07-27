package HugoScale;

public class MyArrayMaths {
	
	public float SumArrayFloat(float[] array) {
	    float sum = 0;
	    for (float value : array) {
	        sum += value;
	    }
	    return sum;
	}
	
	public float AvgArrayFloat(float[] array) {
	    float sum = SumArrayFloat(array);
	    return (float) sum / array.length;
	}

}
