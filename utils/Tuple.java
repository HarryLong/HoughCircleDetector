package utils;

public class Tuple<T1,T2>
{
	private T1 key;
	private T2 value;
	
	public Tuple(T1 el1, T2 el2)
	{
		this.key = el1;
		this.value = el2;
	}
	
	public T1 getKey()
	{
		return key;
	}
	
	public T2 getValue()
	{
		return value;
	}
}
