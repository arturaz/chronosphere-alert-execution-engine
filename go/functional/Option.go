package functional

type Option[A any] struct {
	IsSome      bool
	UnsafeValue A
}

func Some[A any](a A) Option[A] {
	return Option[A]{
		IsSome:      true,
		UnsafeValue: a,
	}
}

func None[A any]() Option[A] {
	return Option[A]{IsSome: false}
}

func (opt *Option[A]) IsNone() bool {
	return !opt.IsSome
}
