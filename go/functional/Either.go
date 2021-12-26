package functional

// Either represents a Left(A) or Right(B) value.
type Either[A any, B any] struct {
	IsRight     bool
	UnsafeLeft  A
	UnsafeRight B
}

func Left[A any, B any](a A) Either[A, B] {
	return Either[A, B]{
		IsRight:    false,
		UnsafeLeft: a,
	}
}

func Right[A any, B any](b B) Either[A, B] {
	return Either[A, B]{
		IsRight:     true,
		UnsafeRight: b,
	}
}

func (e Either[A, B]) Match(onLeft func(a A), onRight func(b B)) {
	if e.IsRight {
		onRight(e.UnsafeRight)
	} else {
		onLeft(e.UnsafeLeft)
	}
}
