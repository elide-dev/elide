def fib(limit=10000, previous=1, current=0):

  ''' Generate items in the fibonacci sequence,
      from the pair (`previous`, `current`) to `limit`.
      All parameters are ``int``s, and one need only
      pass a 'limit,' which is also optional and defaults
      to ``10,000``.
      
      :param limit: Value limit to generate numbers to.
      :param previous: Former value in the moving window.
      :param current: Latter value in the moving window.
      :returns: ``int`` values that are successively greater
      members of the Fibonacci sequence. '''

  if limit and (current >= limit):
    pass
  else:
    yield previous + current
    for next in fib(limit, current, (previous + current)):
      yield next

def fib_as_list(limit=10000): 
  return [i for i in fib(limit)]

