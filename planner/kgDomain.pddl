(define (domain kgDomain)
  (:requirements :strips :typing :derived-predicates)
  (:predicates
    (isat ?x1 ?x2 )
  )

  (:action action0
    :parameters (?y ?x)
    :precondition (and 
        (isat ?x ?y)
    )
    :effect (and 
        (isat ?y ?x)
        (isat newobject ?y)
    )
  )

)
