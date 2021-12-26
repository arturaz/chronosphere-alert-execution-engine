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

func (opt *Option[A]) IfSome(f func(a A)) {
	if opt.IsSome {
		f(opt.UnsafeValue)
	}
}

func (opt *Option[A]) Match(ifNone func(), ifSome func(a A)) {
	if opt.IsSome {
		ifSome(opt.UnsafeValue)
	} else {
		ifNone()
	}
}

func Fold[A any, B any](opt Option[A], ifNone func() B, ifSome func(a A) B) B {
	if opt.IsSome {
		return ifSome(opt.UnsafeValue)
	} else {
		return ifNone()
	}
}
