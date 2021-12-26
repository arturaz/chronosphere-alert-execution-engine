package utils

// IsChannelClosed returns true if the given channel is closed.
type IsChannelClosed func() bool

func ProduceCheckIfChanIsClosedFunc[A any](channel chan A) IsChannelClosed {
	return func() bool {
		_, isOpen := <-channel
		return !isOpen
	}
}
